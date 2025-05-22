import tensorflow as tf
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler
import matplotlib.pyplot as plt
from datetime import datetime
import os

# 配置参数
SEQUENCE_LENGTH = 7  # 输入序列长度（使用7天的数据预测）
PREDICTION_LENGTH = 3  # 预测未来3天
EPOCHS = 100
BATCH_SIZE = 32

def load_and_preprocess_data(csv_file):
    """加载并预处理数据"""
    df = pd.read_csv(csv_file)
    df['Timestamp'] = pd.to_datetime(df['Timestamp'])
    df = df.sort_values('Timestamp')
    
    # 计算每日用电量变化
    df['Change'] = df['Balance'].diff().abs()
    
    # 移除缺失值
    df = df.dropna()
    
    return df

def create_sequences(data, seq_length, pred_length):
    """创建时间序列数据"""
    X, y = [], []
    for i in range(len(data) - seq_length - pred_length + 1):
        X.append(data[i:(i + seq_length)])
        y.append(data[(i + seq_length):(i + seq_length + pred_length)])
    return np.array(X), np.array(y)

def build_model(seq_length, pred_length):
    """构建LSTM模型"""
    model = tf.keras.Sequential([
        tf.keras.layers.LSTM(64, input_shape=(seq_length, 1), return_sequences=True),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.LSTM(32),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(pred_length)
    ])
    
    model.compile(
        optimizer='adam',
        loss='mse',
        metrics=['mae']
    )
    
    return model

def train_model(csv_file, output_dir):
    """训练模型并保存"""
    # 加载数据
    df = load_and_preprocess_data(csv_file)
    data = df['Change'].values
    
    # 数据标准化
    scaler = MinMaxScaler()
    data_scaled = scaler.fit_transform(data.reshape(-1, 1))
    
    # 创建序列
    X, y = create_sequences(data_scaled, SEQUENCE_LENGTH, PREDICTION_LENGTH)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # 构建并训练模型
    model = build_model(SEQUENCE_LENGTH, PREDICTION_LENGTH)
    
    history = model.fit(
        X_train, y_train,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        validation_data=(X_test, y_test),
        callbacks=[
            tf.keras.callbacks.EarlyStopping(
                monitor='val_loss',
                patience=10,
                restore_best_weights=True
            )
        ]
    )
    
    # 保存训练历史
    plt.figure(figsize=(10, 6))
    plt.plot(history.history['loss'], label='Training Loss')
    plt.plot(history.history['val_loss'], label='Validation Loss')
    plt.title('Model Loss')
    plt.xlabel('Epoch')
    plt.ylabel('Loss')
    plt.legend()
    plt.savefig(os.path.join(output_dir, 'training_history.png'))
    
    # 保存模型评估结果
    test_loss = model.evaluate(X_test, y_test)
    with open(os.path.join(output_dir, 'model_evaluation.txt'), 'w') as f:
        f.write(f'Test Loss: {test_loss[0]}\n')
        f.write(f'Test MAE: {test_loss[1]}\n')
    
    # 转换为TFLite模型
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float32]
    tflite_model = converter.convert()
    
    # 保存TFLite模型
    tflite_path = os.path.join(output_dir, 'electricity_predictor.tflite')
    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)
    
    # 保存数据预处理参数
    preprocessing_params = {
        'scaler_min': scaler.data_min_[0],
        'scaler_max': scaler.data_max_[0]
    }
    np.save(os.path.join(output_dir, 'preprocessing_params.npy'), preprocessing_params)
    
    return model, scaler

def test_tflite_model(tflite_path, test_data, scaler):
    """测试TFLite模型"""
    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    # 准备测试数据
    test_data_scaled = scaler.transform(test_data.reshape(-1, 1))
    test_sequence = test_data_scaled[-SEQUENCE_LENGTH:].reshape(1, SEQUENCE_LENGTH, 1)
    
    # 运行推理
    interpreter.set_tensor(input_details[0]['index'], test_sequence.astype(np.float32))
    interpreter.invoke()
    predictions = interpreter.get_tensor(output_details[0]['index'])
    
    # 反向转换预测结果
    predictions_original = scaler.inverse_transform(predictions.reshape(-1, 1))
    
    return predictions_original

if __name__ == '__main__':
    # 设置输出目录
    output_dir = 'model_output'
    os.makedirs(output_dir, exist_ok=True)
    
    # 训练模型
    model, scaler = train_model('balance_data.csv', output_dir)
    
    # 测试TFLite模型
    test_data = np.random.rand(10)  # 示例测试数据
    predictions = test_tflite_model(
        os.path.join(output_dir, 'electricity_predictor.tflite'),
        test_data,
        scaler
    )
    print("Test Predictions:", predictions) 