"use client";
// components/car/CarImageGallery.tsx
// Image gallery for car detail page.
// Source: tasks.md T041; FR-043

import { useState } from "react";
import Image from "next/image";

interface CarImageGalleryProps {
  /** Array of full image URLs (CDN-resolved). */
  images: string[];
  fallbackImage: string;
  alt: string;
}

export function CarImageGallery({
  images,
  fallbackImage,
  alt,
}: CarImageGalleryProps) {
  const [activeIndex, setActiveIndex] = useState(0);
  const safeImages = images.length > 0 ? images : [fallbackImage];
  const hasMultiple = safeImages.length > 1;

  return (
    <div className="space-y-3">
      {/* Main image */}
      <div className="relative aspect-video w-full overflow-hidden rounded-[var(--radius-lg)] bg-muted">
        <Image
          src={safeImages[activeIndex]!}
          alt={alt}
          fill
          priority={activeIndex === 0}
          className="object-cover"
          sizes="(max-width: 768px) 100vw, 55vw"
        />
      </div>

      {/* Thumbnails */}
      {hasMultiple && (
        <div
          role="tablist"
          aria-label="Galeria de imagens"
          className="flex gap-2 overflow-x-auto"
        >
          {safeImages.map((src, i) => (
            <button
              key={i}
              type="button"
              role="tab"
              aria-selected={i === activeIndex}
              aria-label={`Ver imagem ${i + 1}`}
              onClick={() => setActiveIndex(i)}
              className={`relative h-16 w-24 shrink-0 overflow-hidden rounded-[var(--radius-md)] border-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${
                i === activeIndex
                  ? "border-primary"
                  : "border-transparent hover:border-border"
              }`}
            >
              <Image
                src={src}
                alt={`${alt} — miniatura ${i + 1}`}
                fill
                className="object-cover"
                sizes="96px"
              />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
