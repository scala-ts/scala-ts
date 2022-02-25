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

export type WeekDay = 'Mon' | 'Tue' | 'Wed' | 'Thu' | 'Fri' | 'Sat' | 'Sun';

export const WeekDayValues = [ 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun' ];

export function isWeekDay(v: any): v is WeekDay {
  return (
    v == 'Mon' ||
    v == 'Tue' ||
    v == 'Wed' ||
    v == 'Thu' ||
    v == 'Fri' ||
    v == 'Sat' ||
    v == 'Sun'
  );
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
