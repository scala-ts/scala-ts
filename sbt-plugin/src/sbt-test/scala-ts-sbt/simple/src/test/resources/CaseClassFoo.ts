export interface CaseClassFoo {
  id: string;
  name: string;
  i: number;
  flag: number;
  score: number;
  time: number;
  localDate: Date;
  instant: Date;
  localDateTime: Date;
  offsetDateTime: Date;
  zonedDateTime: Date;
  ts: Date;
  tuple2: [string, number];
  tuple3: [Date, Date, CaseClassFoo];
}
