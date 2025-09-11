import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/api/v1/authentication",
  withCredentials: true,
  timeout: 10000,
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message || error.message || "Something went wrong";
    return Promise.reject({ ...error, message });
  }
);

export default api;
