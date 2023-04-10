package io.github.daggerok.springbootintegrationfileexample;

import io.github.daggerok.springbootintegrationfileexample.infrastructure.AbstractSpringBootTest;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@DirtiesContext
@DisplayName("FilesInboundAdapter test")
@AllArgsConstructor(onConstructor_ = @Autowired)
public class FilesInboundAdapterTest extends AbstractSpringBootTest {

    AppProperties props;
    QueueChannel filesChannel;

    @Test
    @SneakyThrows
    void should_receive_message_by_queue_channel() {
        // given
        val inputFile = Paths.get(props.getInputDir(), "should_receive_message_by_queue_channel.txt").toFile();
        try (val writer = new BufferedWriter(new FileWriter(inputFile, false))) {
            if (new File(props.getInputDir()).mkdirs()) {
                log.info("Created {} dir", props.getInputDir());
            }
            log.info("Writing input file...");
            writer.write("Hello, World!");
        }

        // when
        Message<?> message = filesChannel.receive();
        log.info("received a message by QueueChannel: {}", message);

        // then
        val maybePayload = Optional.ofNullable(message).map(Message::getPayload).map(String::valueOf);
        assertThat(maybePayload).isPresent();

        // and
        val payload = maybePayload.orElseThrow(RuntimeException::new);
        assertThat(payload).isInstanceOf(String.class);
        assertThat(String.valueOf(payload)).isEqualTo("Hello, World!");
    }
}
