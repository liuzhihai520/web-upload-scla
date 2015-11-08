package controllers

import java.awt.image.BufferedImage
import java.io.{InputStreamReader, BufferedReader, FileInputStream, File}
import javax.imageio.ImageIO

import org.apache.commons.io.FilenameUtils
import play.api.libs.json.Json
import play.api.mvc._

import scala.sys.process.Process

object Application extends Controller {

    //路劲
    val PATH = "/home/resource/"
    //图片规格
    val size = Array("70x70", "160x160")
    //参数
    val K_USER_ID = "userId"
    val K_FILE_UPLOAD = "Filedata"
    val K_FILE_TYPE = "filetype"
    //哈希值
    val HEXES = "0123456789ABCDEF"

    //返回值
    val mission_param = Ok(Json.stringify(Json.parse( """{"status" : 1, "message" : "missing params"}""")))
        .as("text/json").withHeaders((CACHE_CONTROL, "no-cache"))

    val mission_file = Ok(Json.stringify(Json.parse( """{"status" : 1, "message" : "missing file"}""")))
        .as("text/json").withHeaders((CACHE_CONTROL, "no-cache"))

    val success = Ok(Json.stringify(Json.parse( """{"status" : 0, "message" : "success"}""")))
        .as("text/json").withHeaders((CACHE_CONTROL, "no-cache"))

    //上传图片
    def upload = Action(parse.multipartFormData) {
        request =>
        var result = success
        val formData = request.body.asFormUrlEncoded
        val userId = formData.get(K_USER_ID).map(_.head).getOrElse("0")
        val filetype = formData.get(K_FILE_TYPE).map(_.head).getOrElse("0")
        filetype match {
            case "banner" => {
                request.body.file(K_FILE_UPLOAD).map { f =>
                    val hash = fileHash(f.ref.file).toLowerCase()
                    f.ref.moveTo(new File(createPath(s"banner/$hash.jpg")), replace = true)
                    result = Ok(Json.stringify(Json.parse(s"""{"status" : 0, "message" :"success","url":"banner/$hash.jpg"}"""))).withHeaders((CACHE_CONTROL, "no-cache"))
                } getOrElse {
                    result = mission_file
                }
            }
        }
        result
    }

    //创建路劲
    def createPath(filePath: String): String = {
        val path = FilenameUtils.getPath(filePath)
        val name = FilenameUtils.getBaseName(filePath)
        val ext = FilenameUtils.getExtension(filePath)

        if (isWindows) {
            new File(s"$PATH$path").mkdirs()
        } else {
            Process(s"mkdir -p $PATH$path").!
        }

        PATH + path + name + "." + ext
    }

    //文件哈希
    def fileHash(file: File): String = {
        val input = new StringBuffer()
        val stream = new FileInputStream(file)
        val buffer = new BufferedReader(new InputStreamReader(stream))
        while (buffer.ready()) {
            input.append(buffer.readLine() + "\n")
        }
        buffer.close()
        byteAryToHexStr(getHash(input.toString(), "MD5"))
    }

    def byteAryToHexStr(input: Array[Byte]): String = {
        val hex = new StringBuffer(2 * input.length)
        input.foreach({ f =>
            hex.append(HEXES.charAt((f & 0xF0) >> 4)).append(HEXES.charAt((f & 0x0F)))
        })
        return hex.toString()
    }

    def getHash(toHash: String, technique: String): Array[Byte] = {
        val parse = java.security.MessageDigest.getInstance(technique)
        parse.reset()
        parse.update(toHash.getBytes())
        return parse.digest()
    }


    def isWindows(): Boolean = {
        if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) {
            true;
        } else {
            false
        }
    }

    //删除文件
    def deleteFile(filePath: String) = {
        if (isWindows) {
            new File(filePath).delete()
        } else {
            (Process("rm -rf " + filePath)).!
        }
    }

    //按比例生成图片文件
    def thumbnails(from: String, to: String, size: String) = {
        Process( s"""convert -strip +profile "*" -quality 90 -resize $size $from[0] $to""").!
    }

    //图片裁剪
    def cropperWidth(from: String, to: String, maxWidth: String) = {
        Process( s"""convert -strip +profile "*" -quality 90 -resize $maxWidth $from[0] $to""").!
    }

    //图片缩放
    def zoomThumb(from: String, to: String, size: String) = {
        Process( s"""convert -sample $size $from[0] $to""").!
    }

    def crossdomain = Action {
        implicit viewData =>
            Ok("<cross-domain-policy><site-control permitted-cross-domain-policies=\"all\"/><allow-access-from domain=\"*\"/><allow-http-request-headers-from domain=\"*\" headers=\"*\"/></cross-domain-policy>").as("text/xml")
    }

    def test = Action {
        implicit request =>
            Ok(views.html.index())
    }
}
