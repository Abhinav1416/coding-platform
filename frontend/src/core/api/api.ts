import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080", 
  withCredentials: true,
  timeout: 10000,
});

api.interceptors.request.use(
  (config) => {
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // 1. Get the refresh token from local storage.
        const refreshToken = localStorage.getItem('refreshToken');

        if (!refreshToken) {
            console.error("Interceptor: Could not find refresh token in local storage.");
            throw new Error("No refresh token"); 
        }

        // 2. Make the refresh call, sending the token in the BODY.
        const refreshResponse = await axios.post(
          "http://localhost:8080/api/v1/authentication/refresh-access-token",
          { refreshToken: refreshToken } // <-- THE FIX IS HERE
        );
        
        const { accessToken } = refreshResponse.data;
        localStorage.setItem('accessToken', accessToken);

        originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
        return api(originalRequest);

      } catch (refreshError) {
        console.error("Token refresh failed, logging out:", refreshError);
        localStorage.removeItem('accessToken');
        // Also remove the failed refresh token
        localStorage.removeItem('refreshToken'); 
        
        window.location.href = '/login?sessionExpired=true';

        return Promise.reject(refreshError);
      }
    }

    const message =
      error.response?.data?.message || error.message || "Something went wrong";
    return Promise.reject({ ...error, message });
  }
);

export default api;