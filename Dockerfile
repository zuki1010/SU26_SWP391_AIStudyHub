# Bước 1: Sử dụng Eclipse Temurin với JDK 21 trên nền Alpine Linux (siêu nhẹ, chỉ khoảng ~100MB)
FROM eclipse-temurin:21-jdk-alpine

# Bước 2: Tạo thư mục làm việc bên trong server ảo của Render
WORKDIR /app

# Bước 3: Copy file .jar được tạo ra từ thư mục target vào server ảo
# (Lệnh mvn clean package của Maven sẽ tạo ra file này)
COPY target/*.jar app.jar

# Bước 4: Ra lệnh cho server chạy file jar bằng Java 21
ENTRYPOINT ["java", "-jar", "app.jar"]