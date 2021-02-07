export type ModalProps = {
  id: string;
  title: string;
  message: string;
  headerClass?: string;
  bodyClass?: string;
  closeBtnClass?: string;
  extraBtn?: {
    classname: string;
    onclick: () => void;
    label: string;
  };
};
