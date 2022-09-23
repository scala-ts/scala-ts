// TypeScript added at beginning of every generated file
// ...

declare var exports: any;

export const nsBar = exports;

export interface Bar {
  url: string;
}

export function isBar(v: any): v is Bar {
  return (
    ((typeof v['url']) === 'string')
  );
}

export const nsWeekDay = exports;

const WeekDayEntries = {
  Mon: 'Mon',
  Tue: 'Tue',
  Wed: 'Wed',
  Thu: 'Thu',
  Fri: 'Fri',
  Sat: 'Sat',
  Sun: 'Sun',
};

export type WeekDay = keyof (typeof WeekDayEntries);

export const WeekDay = {
  ...WeekDayEntries,
  values: Object.keys(WeekDayEntries)
} as const;

export function isWeekDay(v: any): v is WeekDay {
  return WeekDay.values.includes(v);
}

export const nsLorem = exports;

export interface Lorem {
  year: number;
  weekday: WeekDay;
}

export function isLorem(v: any): v is Lorem {
  return (
    ((typeof v['year']) === 'number') &&
    (v['weekday'] && nsWeekDay.isWeekDay(v['weekday']))
  );
}

export const nsFoo = exports;

export interface Foo {
  name: string;
  bar?: Bar;
  score: ReadonlyArray<number>;
}

export function isFoo(v: any): v is Foo {
  return (
    ((typeof v['name']) === 'string') &&
    (!v['bar'] || (v['bar'] && nsBar.isBar(v['bar']))) &&
    (Array.isArray(v['score']) && v['score'].every(elmt => (typeof elmt) === 'number'))
  );
}
