package com.knowit.policesystem.projection.nats;

import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.consumer.OperationalEventParser;
import com.knowit.policesystem.projection.service.OperationalProjectionService;
import io.nats.client.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalNatsListenerTest {

    @Mock
    private NatsProperties properties;

    @Mock
    private OperationalEventParser parser;

    @Mock
    private OperationalProjectionService projectionService;

    private OperationalNatsListener listener;

    @BeforeEach
    void setUp() {
        listener = new OperationalNatsListener(properties, parser, projectionService);
    }

    @Test
    void start_WhenDisabled_DoesNotConnect() {
        when(properties.isEnabled()).thenReturn(false);

        listener.start();

        // Should not throw exception when disabled
    }

    @Test
    void handleMessage_WithValidEvent_ProcessesEvent() throws Exception {
        Message message = mock(Message.class);
        when(message.getData()).thenReturn("{\"eventType\":\"ReportIncidentRequested\"}".getBytes(StandardCharsets.UTF_8));
        when(message.getSubject()).thenReturn("commands.incident.report");
        when(parser.parse(anyString(), anyString())).thenReturn(new Object());

        // Use reflection to call handleMessage
        Method handleMessage = OperationalNatsListener.class.getDeclaredMethod("handleMessage", Message.class);
        handleMessage.setAccessible(true);
        handleMessage.invoke(listener, message);

        verify(parser).parse(anyString(), anyString());
        verify(projectionService).handle(any());
    }

    @Test
    void handleMessage_WithParserException_LogsError() throws Exception {
        Message message = mock(Message.class);
        when(message.getData()).thenReturn("invalid".getBytes(StandardCharsets.UTF_8));
        when(message.getSubject()).thenReturn("commands.incident.report");
        when(parser.parse(anyString(), anyString())).thenThrow(new RuntimeException("Parse error"));

        Method handleMessage = OperationalNatsListener.class.getDeclaredMethod("handleMessage", Message.class);
        handleMessage.setAccessible(true);
        handleMessage.invoke(listener, message);

        verify(parser).parse(anyString(), anyString());
        verify(projectionService, never()).handle(any());
    }

    @Test
    void shutdown_WhenNotStarted_DoesNotThrow() {
        // Should not throw exception when shutdown called without start
        listener.shutdown();
    }
}
