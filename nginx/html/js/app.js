document.getElementById('testBtn').onclick = function() {
    const resultDiv = document.getElementById('result');
    resultDiv.innerText = "请求中...";
    
    // 注意：这里用相对路径 /api，Nginx 会帮我们转发
    fetch('/api/user/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'admin', password: '123' })
    })
    .then(response => response.json())
    .then(data => {
        resultDiv.innerText = "后端响应: " + JSON.stringify(data);
        document.getElementById('status').innerText = "通信正常";
    })
    .catch(err => {
        resultDiv.innerText = "错误: " + err;
        document.getElementById('status').innerText = "通信失败";
    });
};