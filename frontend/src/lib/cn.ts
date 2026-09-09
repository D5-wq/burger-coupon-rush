/** 조건부 className 을 공백으로 합쳐주는 작은 헬퍼. */
export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}
