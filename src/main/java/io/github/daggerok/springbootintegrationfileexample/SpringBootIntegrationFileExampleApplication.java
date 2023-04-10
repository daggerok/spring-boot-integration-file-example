package io.github.daggerok.springbootintegrationfileexample;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.transformer.FileToStringTransformer;

@SpringBootApplication
public class SpringBootIntegrationFileExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootIntegrationFileExampleApplication.class, args);
    }
}

@Configuration
@ConditionalOnMissingClass
@EnableConfigurationProperties(AppProperties.class)
class AppPropertiesConfig {
}

@Data
@Setter(AccessLevel.PACKAGE)
@ConfigurationProperties("app")
class AppProperties {

    final String inputDir;
    final String outputDir;
    final String filenamePattern;
    final Duration syncInterval;
    final boolean deleteSource;
    final boolean createDir;
    final boolean recursive;
    final int bufferSize;

    @ConstructorBinding
    AppProperties(@DefaultValue("target/input-dir") String inputDir,
                  @DefaultValue("target/output-dir") String outputDir,
                  @DefaultValue("*.txt") String filenamePattern,
                  @DefaultValue("1234ms") Duration syncInterval,
                  @DefaultValue("true") boolean deleteSource,
                  @DefaultValue("true") boolean createDir,
                  @DefaultValue("true") boolean recursive,
                  @DefaultValue("1024") int bufferSize) {

        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.syncInterval = syncInterval;
        this.filenamePattern = filenamePattern;
        this.deleteSource = deleteSource;
        this.createDir = createDir;
        this.recursive = recursive;
        this.bufferSize = bufferSize;
    }
}

@Log4j2
@Configuration
class SpringIntegrationInputFileConfig {

    @Bean
    IntegrationFlow readInputDirFilesFlow(FileInboundChannelAdapterSpec readInputDirFilesSpec,
                                          Consumer<SourcePollingChannelAdapterSpec> sourcePollingConsumer,
                                          FileToStringTransformer fileToStringTransformer) {

        return IntegrationFlows.from(readInputDirFilesSpec, sourcePollingConsumer)
                .transform(fileToStringTransformer)
                .channel(filesChannel())
                .get();
    }


    @Bean
    FileInboundChannelAdapterSpec readInputDirFilesSpec(AppProperties props) {
        File inputDirectory = new File(props.getInputDir());
        return Files.inboundAdapter(inputDirectory)
                .patternFilter(props.getFilenamePattern())
                .autoCreateDirectory(props.isCreateDir())
                .recursive(props.isRecursive());
    }

    @Bean
    Consumer<SourcePollingChannelAdapterSpec> sourcePollingSpecConsumer(AppProperties props) {
        return spec -> spec.poller(Pollers.fixedDelay(props.getSyncInterval().toMillis()));
    }

    @Bean
    FileToStringTransformer fileToStringTransformer(AppProperties props) {
        return Files.toStringTransformer(StandardCharsets.UTF_8.displayName(), props.isDeleteSource());
    }

    @Bean
    QueueChannel filesChannel() {
        return new QueueChannel();
    }

    @Bean
    IntegrationFlow writeFilesToOutputDirFlow(FileWritingMessageHandler writeFilesToOutputDirHandler) {
        return IntegrationFlows.from(filesChannel())
                .handle(writeFilesToOutputDirHandler)
                .get();
    }

    // @Bean
    // FileWritingMessageHandler writeFilesToOutputDirHandler(AppProperties props) {
    //     val handler = new FileWritingMessageHandler(Paths.get(props.getOutputDir()).toFile());
    //     handler.setCharset(StandardCharsets.UTF_8.displayName());
    //     handler.setDeleteSourceFiles(props.isDeleteSource());
    //     handler.setAutoCreateDirectory(props.isCreateDir());
    //     handler.setBufferSize(props.getBufferSize());
    //     return handler;
    // }

    @Bean
    FileWritingMessageHandler writeFilesToOutputDirHandler(AppProperties props) {
        val destinationDirectory = new File(props.getOutputDir());
        return Files.outboundAdapter(destinationDirectory)
                .charset(StandardCharsets.UTF_8.displayName())
                .autoCreateDirectory(props.isCreateDir())
                .get();
    }

    @Bean
    PublishSubscribeChannel errorChannel() {
        return new PublishSubscribeChannel();
    }
}
