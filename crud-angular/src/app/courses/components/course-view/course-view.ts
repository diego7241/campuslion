
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  inject,
  input,
  linkedSignal,
  signal,
  viewChild
} from '@angular/core';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatDrawer, MatSidenavModule } from '@angular/material/sidenav';
import { fromEvent } from 'rxjs';
import { YouTubePlayerModule } from '@angular/youtube-player';

import { Course } from '../../model/course';
import { Lesson } from '../../model/lesson';

@Component({
  selector: 'app-course-view',
  templateUrl: './course-view.html',
  styleUrl: './course-view.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatSidenavModule,
    MatButtonModule,
    MatListModule,
    YouTubePlayerModule
  ]
})
export class CourseView {
  course = input.required<Course>();

  protected isMobile = signal(false);

  protected selectedLesson = linkedSignal<Lesson | null>(
    () => this.course().lessons?.[0] ?? null
  );

  protected videoHeight = signal(0);
  protected videoWidth = signal(0);

  protected youTubePlayer =
    viewChild<ElementRef<HTMLDivElement>>('youTubePlayer');

  protected drawer = viewChild<MatDrawer>('drawer');

  private readonly destroyRef = inject(DestroyRef);
  private resizeObserver?: ResizeObserver;

  constructor() {
    afterNextRender(() => {
      this.onResize();

      const element = this.youTubePlayer()?.nativeElement;

      if (element && typeof ResizeObserver !== 'undefined') {
        this.resizeObserver = new ResizeObserver(() => {
          this.updateVideoSize();
        });

        this.resizeObserver.observe(element);
      }
    });

    fromEvent(window, 'resize')
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.onResize());

    this.destroyRef.onDestroy(() => {
      this.resizeObserver?.disconnect();
    });
  }

  protected onResize(): void {
    this.isMobile.set(window.innerWidth <= 767);
    this.updateVideoSize();
  }

  private updateVideoSize(): void {
    const element = this.youTubePlayer();

    if (!element) return;

    const availableWidth = element.nativeElement.clientWidth;

    const width = Math.floor(
      Math.max(0, Math.min(availableWidth * 0.95, 960))
    );

    this.videoWidth.set(width);
    this.videoHeight.set(Math.floor(width * 9 / 16));
  }

  protected display(lesson: Lesson): void {
    this.selectedLesson.set(lesson);

    if (this.isMobile()) {
      this.drawer()?.close();
    }
  }

  protected displaySelectedLesson(lesson: Lesson): boolean {
    return this.selectedLesson() === lesson;
  }
}
