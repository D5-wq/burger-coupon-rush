export default function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-20 text-ink-faint">
      <span className="h-8 w-8 animate-spin rounded-full border-[3px] border-brand border-t-transparent" />
      {label && <p className="text-sm">{label}</p>}
    </div>
  );
}
