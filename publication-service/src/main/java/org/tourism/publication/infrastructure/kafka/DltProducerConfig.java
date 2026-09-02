package org.tourism.publication.infrastructure.kafka;

import java.util.Map;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.tourism.publication.contract.ProtocolSnapshot;

/**
 * Продюсер, используемый только для отправки отбракованных сообщений в DLT.
 *
 * <p>Значение сериализуется по-разному в зависимости от того, почему запись упала: при ошибке
 * бизнес-проверки это разобранный {@link ProtocolSnapshot}, при ошибке десериализации — исходные
 * байты, которые разобрать не удалось.
 */
@Configuration
public class DltProducerConfig {

  @Bean
  public ProducerFactory<String, Object> dltProducerFactory(KafkaProperties kafkaProperties) {
    Map<Class<?>, Serializer<?>> delegates =
        Map.of(
            byte[].class, new ByteArraySerializer(),
            ProtocolSnapshot.class, new JacksonJsonSerializer<>());

    return new DefaultKafkaProducerFactory<>(
        kafkaProperties.buildProducerProperties(),
        new StringSerializer(),
        new DelegatingByTypeSerializer(delegates));
  }

  @Bean
  public KafkaTemplate<String, Object> dltKafkaTemplate(
      ProducerFactory<String, Object> dltProducerFactory) {
    return new KafkaTemplate<>(dltProducerFactory);
  }

  @Bean
  public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
      KafkaTemplate<String, Object> dltKafkaTemplate) {
    return new DeadLetterPublishingRecoverer(dltKafkaTemplate);
  }
}
