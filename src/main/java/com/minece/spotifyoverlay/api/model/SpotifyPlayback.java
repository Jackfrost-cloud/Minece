package com.minece.spotifyoverlay.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class SpotifyPlayback {
    @SerializedName("is_playing")
    private boolean isPlaying;

    @SerializedName("progress_ms")
    private long progressMs;

    @SerializedName("item")
    private Track item;

    public boolean isPlaying() {
        return isPlaying;
    }

    public long getProgressMs() {
        return progressMs;
    }

    public long getDurationMs() {
        return item != null ? item.getDurationMs() : 0L;
    }

    public boolean hasTrack() {
        return item != null;
    }

    public String getTrackName() {
        return item != null ? item.getName() : "Nothing playing";
    }

    public String getArtistName() {
        if (item == null) {
            return "";
        }
        return item.getArtistNames();
    }

    public String getAlbumArtUrl() {
        return item != null && item.getAlbum() != null ? item.getAlbum().getBestImageUrl() : "";
    }

    public String getSpotifyUrl() {
        return item != null ? item.getSpotifyUrl() : "";
    }

    public String getTrackId() {
        return item != null ? item.getId() : "";
    }

    public static class Track {
        private String id;
        private String name;

        @SerializedName("duration_ms")
        private long durationMs;

        private List<Artist> artists;
        private Album album;

        @SerializedName("external_urls")
        private Map<String, String> externalUrls;

        public String getName() {
            return name;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public String getArtistNames() {
            if (artists == null || artists.isEmpty()) {
                return "Unknown artist";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < artists.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(artists.get(i).getName());
            }
            return builder.toString();
        }

        public Album getAlbum() {
            return album;
        }

        public String getSpotifyUrl() {
            if (externalUrls == null) {
                return "";
            }
            return externalUrls.get("spotify");
        }

        public String getId() {
            return id;
        }
    }

    public static class Artist {
        private String name;

        public String getName() {
            return name;
        }
    }

    public static class Album {
        private List<Image> images;

        public List<Image> getImages() {
            return images;
        }

        public String getBestImageUrl() {
            if (images == null || images.isEmpty()) {
                return "";
            }
            Image best = images.get(0);
            for (Image image : images) {
                if (image.getHeight() >= best.getHeight()) {
                    best = image;
                }
            }
            return best.getUrl();
        }
    }

    public static class Image {
        private String url;
        private int width;
        private int height;

        public String getUrl() {
            return url;
        }

        public int getHeight() {
            return height;
        }
    }
}
