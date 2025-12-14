package com.knowit.policesystem.edge.commands.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.BatchCreateOfficersRequestDto;

/**
 * Command for batch registering officers.
 * Processed by BatchRegisterOfficersCommandHandler.
 */
public class BatchRegisterOfficersCommand extends Command {

    private BatchCreateOfficersRequestDto requestDto;

    /**
     * Default constructor for deserialization.
     */
    public BatchRegisterOfficersCommand() {
        super();
    }

    /**
     * Creates a new batch register officers command.
     *
     * @param aggregateId the aggregate identifier (can be a batch ID or first officer badge number)
     * @param requestDto the batch request DTO
     */
    public BatchRegisterOfficersCommand(String aggregateId, BatchCreateOfficersRequestDto requestDto) {
        super(aggregateId);
        this.requestDto = requestDto;
    }

    @Override
    public String getCommandType() {
        return "BatchRegisterOfficersCommand";
    }

    public BatchCreateOfficersRequestDto getRequestDto() {
        return requestDto;
    }

    public void setRequestDto(BatchCreateOfficersRequestDto requestDto) {
        this.requestDto = requestDto;
    }
}
