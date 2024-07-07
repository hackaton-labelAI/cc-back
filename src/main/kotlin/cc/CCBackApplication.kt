package cc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties
class CCBackApplication

object CCBackDev {
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("spring.profiles.active", "dev,local")
        System.setProperty("spring.config.location", "src/main/conf/")
        System.setProperty("config.location", "src/main/conf/")
        System.setProperty("logging.config", "src/main/conf/logback.xml")
        System.setProperty("log.dir", "/tmp/")

        cc.main(args)
    }
}

fun main(args: Array<String>) {
    System.setProperty("user.timezone", "UTC")
    System.setProperty("file.encoding", "UTF-8")

    runApplication<CCBackApplication>(*args)
}
