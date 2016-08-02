package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mmb on 2016/8/1.
 */
@RestController
public class Controller {

   @Value("${words}") String words;

    @RequestMapping("/")
    public  String getWord() {
        String[] wordArray = words.split(",");
        int i = (int)Math.round(Math.random() * (wordArray.length - 1));
        return wordArray[i];
    }
}
