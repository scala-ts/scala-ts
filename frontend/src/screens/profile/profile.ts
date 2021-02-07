export const signOut = () => {
  localStorage.removeItem("scala-ts-demo.token");
  location.href = "/signin";
};
