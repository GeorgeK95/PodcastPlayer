package com.georgek.sgfs.podcastplayer.service

import com.georgek.sgfs.podcastplayer.model.response.RssFeedResponse
import com.georgek.sgfs.podcastplayer.util.DateUtils
import okhttp3.*
import org.w3c.dom.Node
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

class RssFeedService : FeedService {

    override fun getFeed(xmlFileURL: String, callBack: (RssFeedResponse?) -> Unit) {
        val client = OkHttpClient()

        val request = Request.Builder()
                .url(xmlFileURL)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callBack(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val documentXmlObject = DocumentBuilderFactory.newInstance()
                                .newDocumentBuilder()
                                .parse(responseBody.byteStream())

                        val rssFeedResponse = RssFeedResponse(episodeModels = mutableListOf())
                        domToRssFeedResponse(documentXmlObject, rssFeedResponse)
                        callBack(rssFeedResponse)
                        println(rssFeedResponse)

                        return
                    }
                }

                callBack(null)
            }

        })
    }

    private fun domToRssFeedResponse(node: Node, rssFeedResponse: RssFeedResponse) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val nodeName = node.nodeName
            val parentName = node.parentNode.nodeName

            val grandParentName = node.parentNode.parentNode?.nodeName ?: ""

            if (parentName == "item" && grandParentName == "channel") {
                //episode element
                val currentItem = rssFeedResponse.episodeModels?.last()
                if (currentItem != null) {
                    when (nodeName) {
                        "title" -> currentItem.title = node.textContent
                        "description" -> currentItem.description = node.textContent
                        "itunes:duration" -> currentItem.duration = node.textContent
                        "guid" -> currentItem.guid = node.textContent
                        "pubDate" -> currentItem.pubDate = node.textContent
                        "link" -> currentItem.link = node.textContent
                        "enclosure" -> {
                            currentItem.url = node.attributes.getNamedItem("url").textContent
                            currentItem.type = node.attributes.getNamedItem("type").textContent
                        }
                    }
                }
            }

            if (parentName == "channel") {
                when (nodeName) {
                    "title" -> rssFeedResponse.title = node.textContent
                    "description" -> rssFeedResponse.description = node.textContent
                    "itunes:summary" -> rssFeedResponse.summary = node.textContent
                    "item" -> rssFeedResponse.episodeModels?.add(RssFeedResponse.EpisodeResponseModel())
                    "pubDate" -> rssFeedResponse.lastUpdated = DateUtils.xmlDateToDate(node.textContent)
                }
            }
        }

        val nodeList = node.childNodes
        for (i in 0 until nodeList.length) {
            val childNode = nodeList.item(i)
            domToRssFeedResponse(childNode, rssFeedResponse)
        }
    }
}

interface FeedService {
    fun getFeed(xmlFileURL: String, callBack: (RssFeedResponse?) -> Unit)

    companion object {
        val instance: FeedService by lazy {
            RssFeedService()
        }
    }
}