export enum TSWeekDay {
  Mon = 'Mon',
  Tue = 'Tue',
  Wed = 'Wed',
  Thu = 'Thu',
  Fri = 'Fri',
  Sat = 'Sat',
  Sun = 'Sun'
}

export const TSWeekDayValues: Array<TSWeekDay> = [
  TSWeekDay.Mon,
  TSWeekDay.Tue,
  TSWeekDay.Wed,
  TSWeekDay.Thu,
  TSWeekDay.Fri,
  TSWeekDay.Sat,
  TSWeekDay.Sun
];

export function isTSWeekDay(v: any): v is TSWeekDay {
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
