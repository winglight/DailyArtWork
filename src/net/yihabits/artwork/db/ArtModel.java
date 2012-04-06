package net.yihabits.artwork.db;

import java.util.Date;

public class ArtModel {

	private long id = -1;
	private String name;
	private String author;
	private String authorDetails;
	private String year;
	private String details;
	private String location;
	private String imageUrl;
	private String imageLocation;
	private String preImageUrl;
	private String preImageLocation;
	private Date createdAt;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getAuthorDetails() {
		return authorDetails;
	}
	public void setAuthorDetails(String authorDetails) {
		this.authorDetails = authorDetails;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	public String getImageLocation() {
		return imageLocation;
	}
	public void setImageLocation(String imageLocation) {
		this.imageLocation = imageLocation;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getPreImageUrl() {
		return preImageUrl;
	}
	public void setPreImageUrl(String preImageUrl) {
		this.preImageUrl = preImageUrl;
	}
	public String getPreImageLocation() {
		return preImageLocation;
	}
	public void setPreImageLocation(String preImageLocation) {
		this.preImageLocation = preImageLocation;
	}
	
	
}
