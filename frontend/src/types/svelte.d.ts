type NativeMouseEvent = MouseEvent;

declare namespace DOM {
  export type MouseEvent<T> = NativeMouseEvent & {
    currentTarget: EventTarget & T;
  };
}
