import { useState } from 'react';
import { gradientFor } from '../lib/format';

interface Props {
  src: string | null;
  name: string;
  className?: string;
}

/** 이미지 로드 실패 시 상품명 기반 그라디언트 + 🍔 로 폴백 → 오프라인에서도 안 깨진다. */
export default function ProductImage({ src, name, className }: Props) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return (
      <div
        className={className}
        style={{ background: gradientFor(name) }}
        aria-label={name}
      >
        <div className="flex h-full w-full items-center justify-center text-4xl drop-shadow-sm">
          🍔
        </div>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={name}
      loading="lazy"
      onError={() => setFailed(true)}
      className={className}
      style={{ objectFit: 'cover' }}
    />
  );
}
