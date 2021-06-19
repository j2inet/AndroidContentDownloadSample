package net.j2i.phone.resourcedownloadsample

import java.net.URL

public class ResourceDownloadRequest {
    public val source: URL
    public val name:String

    public constructor(name:String, source: URL) {
        this.source = source
        this.name = name
    }
}