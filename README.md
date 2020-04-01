# knowledgeBase

Website in which I store all my notes. It is in continuous development. 

## How to build

1. compile the `.jar` file
2. when executing it, specify the directory of the html files with `--dir`, the servers port with `--port` and the path to the `.p12` keystore containing the TLS certificates with `--keystore`, e.g.:
```
~$ java -jar knowledgeBase.jar --dir /home/user/knowledge_base --port 80 --keystore /home/user/knowledge_base/keystore.p12
```

