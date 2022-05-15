// TypeScript added at beginning of every generated file
// ...

export interface Bar {
  url: string;
}

export function isBar(v: any): v is Bar {
  return (
    ((typeof v['url']) === 'string')
  );
}

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

export interface Lorem {
  year: number;
  weekday: WeekDay;
}

export function isLorem(v: any): v is Lorem {
  return (
    ((typeof v['year']) === 'number') &&
    (v['weekday'] && isWeekDay(v['weekday']))
  );
}

export interface Foo {
  name: string;
  bar?: Bar;
  score: ReadonlyArray<number>;
}

export function isFoo(v: any): v is Foo {
  return (
    ((typeof v['name']) === 'string') &&
    (!v['bar'] || (v['bar'] && isBar(v['bar']))) &&
    (Array.isArray(v['score']) && v['score'].every(elmt => (typeof elmt) === 'number'))
  );
}
