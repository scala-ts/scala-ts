// TypeScript added at beginning of every generated file
// ...

export interface Bar {
  url: string;
}

export enum WeekDay {
  Mon = 'Mon',
  Tue = 'Tue',
  Wed = 'Wed',
  Thu = 'Thu',
  Fri = 'Fri',
  Sat = 'Sat',
  Sun = 'Sun'
}

export interface Lorem {
  year: number;
  weekday: WeekDay;
}

export interface Foo {
  name: string;
  bar?: Bar;
  score: ReadonlyArray<number>;
}
