// TypeScript added at beginning of every generated file
// ...

export interface Bar {
  url: string;
}

export type WeekDay = 'Mon' | 'Tue' | 'Wed' | 'Thu' | 'Fri' | 'Sat' | 'Sun'

export const WeekDayValues = [ 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun' ]

export interface Lorem {
  year: number;
  weekday: WeekDay;
}

export interface Foo {
  name: string;
  bar?: Bar;
  score: ReadonlyArray<number>;
}
