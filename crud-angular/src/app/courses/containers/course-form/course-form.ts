
import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  ChangeDetectionStrategy,
  inject,
  input,
  linkedSignal
} from '@angular/core';

import {
  FormField,
  applyEach,
  form,
  maxLength,
  minLength,
  required,
  submit,
  validate
} from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatOptionModule } from '@angular/material/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';

import { firstValueFrom } from 'rxjs';

import { Course } from '../../model/course';
import { Lesson } from '../../model/lesson';
import { CoursesService } from '../../services/courses';
import { ErrorDialog } from '../../../shared/components/error-dialog/error-dialog';

interface CourseModel {
  _id: string;
  name: string;
  category: string;
  lessons: Lesson[];
}

const NAME_MAX_LENGTH = 150;
const YOUTUBE_ID_PATTERN = /^[A-Za-z0-9_-]{11}$/;

const REQUIRED_MESSAGE = 'Field is required.';

const minLengthMessage = (length: number) =>
  `Field cannot be less than ${length} characters long.`;

const maxLengthMessage = (length: number) =>
  `Field cannot be more than ${length} characters long.`;

/**
 * Obtiene el ID del video desde un enlace completo,
 * enlace corto o ID directo de YouTube.
 */
function extractYouTubeVideoId(value: string): string | null {
  const raw = value.trim();

  if (!raw) return null;

  // ID directo de YouTube.
  if (YOUTUBE_ID_PATTERN.test(raw)) {
    return raw;
  }

  let urlCandidate = raw;

  // Permite enlaces sin https://
  if (!/^https?:\/\//i.test(urlCandidate)) {
    if (/^(www\.)?(youtube\.com|youtu\.be)\//i.test(urlCandidate)) {
      urlCandidate = `https://${urlCandidate}`;
    } else {
      return null;
    }
  }

  try {
    const url = new URL(urlCandidate);

    const hostname = url.hostname
      .toLowerCase()
      .replace(/^www\./, '');

    let videoId = '';

    if (hostname === 'youtu.be') {
      videoId = url.pathname.split('/').filter(Boolean)[0] ?? '';
    } else if (
      [
        'youtube.com',
        'm.youtube.com',
        'music.youtube.com',
        'youtube-nocookie.com'
      ].includes(hostname)
    ) {
      if (url.pathname === '/watch') {
        videoId = url.searchParams.get('v') ?? '';
      } else {
        const match = url.pathname.match(
          /^\/(?:embed|shorts|live)\/([A-Za-z0-9_-]{11})(?:\/|$)/
        );

        videoId = match?.[1] ?? '';
      }
    } else {
      return null;
    }

    return YOUTUBE_ID_PATTERN.test(videoId) ? videoId : null;

  } catch {
    return null;
  }
}

@Component({
  selector: 'app-course-form',
  templateUrl: './course-form.html',
  styleUrl: './course-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    MatCardModule,
    MatToolbarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatOptionModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatDialogModule
  ]
})
export class CourseForm {

  course = input.required<Course>();

  protected readonly nameMaxLength = NAME_MAX_LENGTH;

  private service = inject(CoursesService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private location = inject(Location);

  private model = linkedSignal<CourseModel>(() =>
    this.toModel(this.course())
  );

  protected courseForm = form(this.model, path => {

    required(path.name, {
      message: REQUIRED_MESSAGE
    });

    minLength(path.name, 5, {
      message: minLengthMessage(5)
    });

    maxLength(path.name, NAME_MAX_LENGTH, {
      message: maxLengthMessage(NAME_MAX_LENGTH)
    });

    required(path.category, {
      message: REQUIRED_MESSAGE
    });

    minLength(path.lessons, 1, {
      message: 'At least one lesson is required.'
    });

    applyEach(path.lessons, lesson => {

      required(lesson.name, {
        message: REQUIRED_MESSAGE
      });

      minLength(lesson.name, 5, {
        message: minLengthMessage(5)
      });

      maxLength(lesson.name, 100, {
        message: maxLengthMessage(100)
      });

      required(lesson.youtubeUrl, {
        message: REQUIRED_MESSAGE
      });

      // No se aplica maxLength al enlace.
      // Se valida el ID extraído del enlace completo.
      validate(lesson.youtubeUrl, ({ value }) => {

        const inputValue = value().trim();

        if (!inputValue) {
          return undefined;
        }

        if (!extractYouTubeVideoId(inputValue)) {
          return {
            kind: 'youtubeUrl',
            message: 'Pega un enlace válido de YouTube o un ID de video de 11 caracteres.'
          };
        }

        return undefined;
      });

    });

  });

  /**
   * Prepara el formulario.
   * Los IDs existentes se muestran como enlaces completos
   * para facilitar la edición.
   */
  private toModel(course: Course): CourseModel {

    return {
      _id: course._id ?? '',
      name: course.name ?? '',
      category: course.category ?? '',

      lessons: course.lessons?.length
        ? course.lessons.map(lesson => {

            const videoId = extractYouTubeVideoId(
              lesson.youtubeUrl ?? ''
            );

            return {
              ...lesson,
              youtubeUrl: videoId
                ? `https://www.youtube.com/watch?v=${videoId}`
                : lesson.youtubeUrl
            };

          })
        : [
            {
              _id: 0,
              name: '',
              youtubeUrl: ''
            }
          ]
    };
  }

  protected addLesson() {

    this.model.update(course => ({
      ...course,
      lessons: [
        ...course.lessons,
        {
          _id: 0,
          name: '',
          youtubeUrl: ''
        }
      ]
    }));

  }

  protected removeLesson(index: number) {

    this.model.update(course => ({
      ...course,
      lessons: course.lessons.filter((_, i) => i !== index)
    }));

  }

  protected onSubmit() {

    return submit(this.courseForm, async () => {

      try {

        const currentModel = this.model();

        // Convertir todos los enlaces al ID que
        // actualmente utiliza el backend y el reproductor.
        const courseToSave: CourseModel = {
          ...currentModel,

          lessons: currentModel.lessons.map(lesson => {

            const videoId = extractYouTubeVideoId(
              lesson.youtubeUrl
            );

            if (!videoId) {
              throw new Error('Invalid YouTube video link.');
            }

            return {
              ...lesson,
              youtubeUrl: videoId
            };

          })
        };

        await firstValueFrom(
          this.service.save(courseToSave)
        );

        this.onSuccess();

      } catch (err) {

        this.onError(this.extractErrorMessage(err));

      }

      return undefined;

    });

  }

  private extractErrorMessage(err: unknown): string {

    if (err instanceof Error && !(err instanceof HttpErrorResponse)) {
      return err.message;
    }

    const detail = (err as HttpErrorResponse)?.error?.detail;

    return typeof detail === 'string' && detail
      ? detail
      : 'Error saving course.';
  }

  protected onCancel() {
    this.location.back();
  }

  private onSuccess() {

    this.snackBar.open(
      'Course saved successfully!',
      '',
      { duration: 5000 }
    );

    this.onCancel();

  }

  private onError(message: string) {

    this.dialog.open(ErrorDialog, {
      data: message
    });

  }

}
  