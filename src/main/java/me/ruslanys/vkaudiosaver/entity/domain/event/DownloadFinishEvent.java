package me.ruslanys.vkaudiosaver.entity.domain.event;

import lombok.Getter;
import me.ruslanys.vkaudiosaver.entity.Audio;

import java.util.Collections;
import java.util.List;

public class DownloadFinishEvent extends DownloadEvent {

    @Getter
    private final List<Audio> audioList;

    public DownloadFinishEvent(Object source, List<Audio> audioList) {
        super(source);
        this.audioList = Collections.unmodifiableList(audioList);
    }

}
