package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        if (requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(ParticipationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("The initiator cannot add a request to participate in his event");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("You cannot participate in an unpublished event");
        }

        if (event.getParticipantLimit() > 0) {
            long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("The participant limit has been reached");
            }
        }

        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("You cannot add a repeat request");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        return ParticipationRequestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request with id=" + requestId + " was not found");
        }

        if (request.getStatus() == RequestStatus.CANCELED) {
            return ParticipationRequestMapper.toParticipationRequestDto(request);
        }

        boolean wasConfirmed = request.getStatus() == RequestStatus.CONFIRMED;

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        if (wasConfirmed) {
            Event event = request.getEvent();
            if (event.getConfirmedRequests() > 0) {
                event.setConfirmedRequests(event.getConfirmedRequests() - 1);
                eventRepository.save(event);
            }
        }

        return ParticipationRequestMapper.toParticipationRequestDto(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsForEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        if (requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(ParticipationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Event does not require moderation");
        }

        if (updateRequest.getStatus() == null) {
            throw new ConflictException("Invalid status value: null");
        }

        if (!updateRequest.getStatus().equals("CONFIRMED") && !updateRequest.getStatus().equals("REJECTED")) {
            throw new ConflictException("Invalid status value: " + updateRequest.getStatus());
        }

        if (updateRequest.getRequestIds() == null || updateRequest.getRequestIds().isEmpty()) {
            throw new ValidationException("Request IDs cannot be empty");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());

        if (requests.isEmpty()) {
            throw new NotFoundException("No requests found with provided IDs");
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Request with id=" + request.getId() + " was not found");
            }
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        int availableSlots = event.getParticipantLimit() - event.getConfirmedRequests();

        if ("CONFIRMED".equals(updateRequest.getStatus())) {
            if (availableSlots <= 0) {
                throw new ConflictException("The participant limit has been reached");
            }

            for (ParticipationRequest request : requests) {
                if (availableSlots > 0) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(request);
                    availableSlots--;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(request);
                }
            }

            if (!confirmedRequests.isEmpty()) {
                event.setConfirmedRequests(event.getConfirmedRequests() + confirmedRequests.size());
                eventRepository.save(event);
            }
        } else if ("REJECTED".equals(updateRequest.getStatus())) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        requestRepository.saveAll(requests);

        return new EventRequestStatusUpdateResult(
                confirmedRequests.stream()
                        .map(ParticipationRequestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList()),
                rejectedRequests.stream()
                        .map(ParticipationRequestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList())
        );
    }
}