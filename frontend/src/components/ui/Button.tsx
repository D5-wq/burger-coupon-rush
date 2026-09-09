import { type ButtonHTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

type Variant = 'primary' | 'secondary' | 'ghost';
type Size = 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  fullWidth?: boolean;
}

const variantCls: Record<Variant, string> = {
  primary: 'bg-brand text-white hover:bg-brand-600 active:bg-brand-700 disabled:bg-brand-200',
  secondary: 'bg-white text-ink border border-black/10 hover:bg-black/[0.03]',
  ghost: 'bg-transparent text-ink-soft hover:bg-black/[0.04]',
};

const sizeCls: Record<Size, string> = {
  md: 'h-11 px-5 text-[15px]',
  lg: 'h-14 px-6 text-base',
};

export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  fullWidth = false,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition',
        'disabled:cursor-not-allowed',
        variantCls[variant],
        sizeCls[size],
        fullWidth && 'w-full',
        className,
      )}
      disabled={disabled || loading}
      {...rest}
    >
      {loading && (
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
      )}
      {children}
    </button>
  );
}
