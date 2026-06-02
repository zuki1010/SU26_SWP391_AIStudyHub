# === GIAI ĐOẠN 1: BUILD CODE ===
# Dùng image Maven kết hợp JDK 21 để đóng gói ứng dụng
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Tạo thư mục làm việc trong máy ảo build
WORKDIR /app

# Copy toàn bộ code từ GitHub vào trong máy ảo build
COPY . .

# Chạy lệnh đóng gói sản phẩm (tạo ra thư mục target và file .jar)
RUN mvn clean package -DskipTests


# === GIAI ĐOẠN 2: CHẠY ỨNG DỤNG ===
# Dùng image JDK 21 siêu nhẹ để chạy ứng dụng trực tuyến
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy file .jar từ Giai đoạn 1 (từ máy ảo build) sang máy ảo chạy này
COPY --from=build /app/target/*.jar app.jar

# Lệnh khởi chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]