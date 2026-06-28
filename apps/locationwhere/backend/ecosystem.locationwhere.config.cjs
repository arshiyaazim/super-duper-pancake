module.exports = {
  apps: [
    {
      name: "locationwhere-backend",
      cwd: "/home/azim/location_where/backend",
      script: "dist/app.js",
      instances: 1,
      exec_mode: "fork",
      kill_timeout: 5000,
      listen_timeout: 10000,
      max_memory_restart: "256M",
      error_file: "/home/azim/location_where/backend/logs/error.log",
      out_file: "/home/azim/location_where/backend/logs/out.log",
      merge_logs: true,
      log_date_format: "YYYY-MM-DD HH:mm:ss Z",
      env: {
        NODE_ENV: "production",
        PORT: 8310,
        HOST: "127.0.0.1"
      }
    }
  ]
};
