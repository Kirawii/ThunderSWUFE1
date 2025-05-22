import requests
import time
import random
import logging
import csv
import os

# 设置日志配置，使用唯一化的日志文件名
log_filename = f'balance_data_{time.strftime("%Y%m%d_%H%M%S")}.log'
logging.basicConfig(
    filename=log_filename,
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
)

# 请求的 URL 和 Headers 保持不变
url = 'https://rhfw.swufe.edu.cn/easytong_app/GetPayAccInfoNew'

headers = {
    'Authorization': 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTEiLCJhY2NOdW0iOiIxMzEzMTciLCJwZXJDb2RlIjoiMTExIiwiZXhwIjoxNzYwMjQxNTMxLCJpYXQiOjE3Mjg3MDU1MzEsImp0aSI6IjM3NzA0YjVjLTQyOTEtNGVlZC05ZThkLTRmOWVkMTM5YTRhYyJ9.Y5XOG6n2wH4pXKlgg1HxPhOXSdr7dNC4ZX0fnPPPeNg',
    'User-Agent': 'Mozilla/5.0 (Linux; Android 14; V2241HA Build/UP1A.231005.007; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/117.0.0.0 Mobile Safari/537.36 ZJYXYwebviewbroswer ZJYXYAndroid tourCustomer/yunmaapp.NET/6.3.7/ym-30a974936ba5e48e03b0775175a54a30',
    'Content-Type': 'application/x-www-form-urlencoded',
    'Accept': 'application/json, text/plain, */*',
    'Origin': 'https://rhfw.swufe.edu.cn',
    'Referer': 'https://rhfw.swufe.edu.cn/easytong_webapp/',
}

cookies = {
    'JSESSIONID': '756c515b-98ec-4c6b-b42e-1d810fc6bf26',
    'SESSION': 'ZmFjMzY0YWQtMDA2Ny00ZTRlLTkxYzYtYTRlZTkxNzk4MGNh',
    'etToken': 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTEiLCJhY2NOdW0iOiIxMzEzMTciLCJwZXJDb2RlIjoiMTExIiwiZXhwIjoxNzYwMjQxNTMxLCJpYXQiOjE3Mjg3MDU1MzEsImp0aSI6IjM3NzA0YjVjLTQyOTEtNGVlZC05ZThkLTRmOWVkMTM5YTRhYyJ9.Y5XOG2n2wH4pXKlgg1HxPhOXSdr7dNC4ZX0fnPPPeNg'
}

data = {
    'AccNum': '0',
    'AreaNo': '信园',
    'BuildingNo': '信园B座',
    'FloorNo': '0',
    'ItemNum': '1',
    'RoomNo': 'XYYB-604',
    'Time': '20241012120009',
    'Sign': 'a9189f1868aa3272583ec3b9f8702524',
    'ContentType': 'application/json'
}

csv_file_path = 'balance_data.csv'
balance_data = []
balance_changes = []
timestamps = []

# 读取CSV最后一行以获取上次的 balance 和 timestamp
def get_last_record_from_csv(file_path):
    if os.path.exists(file_path):
        with open(file_path, mode='r', newline='', encoding='utf-8') as csvfile:
            reader = csv.reader(csvfile)
            rows = list(reader)
            if len(rows) > 1:  # 确保文件有数据
                last_row = rows[-1]
                last_timestamp = last_row[0]
                last_balance = float(last_row[1])
                return last_timestamp, last_balance
    return None, None

# 获取上次的记录
last_timestamp, last_balance = get_last_record_from_csv(csv_file_path)

# 如果没有历史数据，初始化为 0
if last_balance is None:
    last_balance = 0

# 创建 CSV 文件并写入表头（仅在文件不存在时）
if not os.path.exists(csv_file_path):
    with open(csv_file_path, mode='w', newline='', encoding='utf-8') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(['Timestamp', 'Balance', 'Change'])  # 加入 'Change' 表头

while True:
    try:
        response = requests.post(url, headers=headers, cookies=cookies, data=data)

        if response.status_code == 200:
            try:
                response_data = response.json()
                balance = float(response_data.get('balance'))  # 获取 balance 数据
                if balance is not None:
                    # 计算变化值
                    if last_balance != 0:
                        change = last_balance - balance
                        if change < 0:
                            change = 0  # 如果balance增加，将减少值设为0
                    else:
                        change = 0

                    # 更新记录
                    last_balance = balance
                    last_timestamp = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())

                    log_message = f'当前时间: {last_timestamp}, Balance: {balance}, 变化值: {change}'
                    print(log_message)
                    logging.info(log_message)

                    # 写入 CSV 文件，保存时间戳、余额和减少值
                    with open(csv_file_path, mode='a', newline='', encoding='utf-8') as csvfile:
                        writer = csv.writer(csvfile)
                        writer.writerow([last_timestamp, balance, change])

                else:
                    logging.warning('未找到 balance 数据')

            except ValueError:
                error_message = '响应不是有效的 JSON'
                print(error_message)
                logging.error(error_message)
                logging.error(response.text)
        else:
            error_message = f'请求失败，状态码：{response.status_code}'
            print(error_message)
            logging.error(error_message)
            logging.error(response.text)

        # 等待 30 到 60 分钟之间的随机时间
        wait_time = random.randint(1800, 3600)  # 600到1200秒
        time.sleep(wait_time)

    except Exception as e:
        error_message = f'发生异常: {str(e)}'
        print(error_message)
        logging.error(error_message)
        time.sleep(5)  # 等待5秒后再重启
