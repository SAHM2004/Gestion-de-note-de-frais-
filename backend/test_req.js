const http = require('http');

const loginOptions = {
  hostname: 'localhost',
  port: 8080,
  path: '/api/auth/login',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  }
};

const req = http.request(loginOptions, res => {
  let data = '';
  res.on('data', chunk => data += chunk);
  res.on('end', () => {
    try {
      const response = JSON.parse(data);
      const token = response.token;
      console.log('Got token');
      
      // Now PUT request
      const putOptions = {
        hostname: 'localhost',
        port: 8080,
        path: '/api/departments/3',
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      };
      const putReq = http.request(putOptions, putRes => {
        let putData = '';
        putRes.on('data', chunk => putData += chunk);
        putRes.on('end', () => {
          console.log(`PUT status: ${putRes.statusCode}`);
          console.log(`PUT response: ${putData}`);
        });
      });
      putReq.write(JSON.stringify({ name: "Service RH", manager: { id: 1 } }));
      putReq.end();
      
    } catch (e) {
      console.error("Parse error", e);
      console.log(data);
    }
  });
});
req.write(JSON.stringify({ email: 'admin@ids-technologie.com', password: 'admin_password_super_secret' }));
req.end();
