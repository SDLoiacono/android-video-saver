FROM openjdk:17

# Instalar herramientas necesarias
RUN apt-get update && apt-get install -y wget unzip

# Configurar directorios y variables de entorno del Android SDK
ENV ANDROID_SDK_ROOT=/sdk
RUN mkdir -p $ANDROID_SDK_ROOT
WORKDIR $ANDROID_SDK_ROOT

# Descargar y extraer las herramientas de línea de comandos de Android
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip && \
    unzip cmdline-tools.zip && \
    rm cmdline-tools.zip && \
    mkdir -p cmdline-tools/latest && \
    mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true && \
    rm -rf cmdline-tools/bin cmdline-tools/lib cmdline-tools/NOTICE.txt cmdline-tools/source.properties

# Agregar herramientas de línea de comandos al PATH
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin

# Aceptar licencias e instalar plataformas y herramientas de compilación requeridas
RUN yes | sdkmanager --licenses
RUN sdkmanager "platforms;android-34" "build-tools;34.0.0" "ndk;25.1.8937393"

# Copiar el proyecto
WORKDIR /project
COPY . /project

# Hacer ejecutable el wrapper de Gradle y compilar APK de release
RUN chmod +x ./gradlew
RUN ./gradlew assembleRelease

# El APK estará disponible en app/build/outputs/apk/release/
