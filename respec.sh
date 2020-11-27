if [ -e "target/nexus-respec.jar" ]; then
  java -XX:MaxHeapFreeRatio=10 -XX:MinHeapFreeRatio=5 -Xms64m -Xmx256m -jar target/nexus-respec.jar "$@"
else
  if [ -e "nexus-respec.jar" ]; then
    java -XX:MaxHeapFreeRatio=10 -XX:MinHeapFreeRatio=5 -Xms64m -Xmx256m -jar nexus-respec.jar "$@"
  fi
fi
