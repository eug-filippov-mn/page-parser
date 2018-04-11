# page-parser
Extracts urls to download any resource in \<HTTP\|HTTPS link\>whitespace\<file name\> format from pages until number of max links reached

#### Requirements
Java 1.8 and higher

#### Build and package
```
./gradlew jar
```
#### Run tests
```
./gradlew test
```

#### Usage 
Run from project directory
```
java -jar build/libs/page-parser-0.1-SNAPSHOT.jar [OPTION]...
```

Option | Description |
-------| ----------- |
-h , --help | Print help information
-o, --out-file-path=OUT_FILE_PATH  | Path to out file in which links will be written. File's line format is \<HTTP\|HTTPS link\>whitespace\<file name\>
-n, --threads-number=THREADS_NUMBER | Number of threads for concurrent pages parsing
-m,--max-links-number=MAX_LINKS_NUMBER | Max links number to write to file with links
-u,--start-url=START_URL | Url to page from which parsing will be started

