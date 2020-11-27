@ECHO OFF
IF EXIST "target\nexus-respec.jar" (
  java -XX:MaxHeapFreeRatio=10 -XX:MinHeapFreeRatio=5 -Xms64m -Xmx256m -jar target/nexus-respec.jar %*
  BREAK
) ELSE (
  IF EXIST "nexus-respec.jar" (
    java -XX:MaxHeapFreeRatio=10 -XX:MinHeapFreeRatio=5 -Xms64m -Xmx256m -jar nexus-respec.jar %*
    BREAK
  )
)
