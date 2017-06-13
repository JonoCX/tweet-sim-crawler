package br.les.opus.dengue.crawler.twitter;

import br.les.opus.twitter.repositories.TweetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author Jonathan Carlton
 */
public class TwitterBot
{
    // We've noticed that you've posted something important. Please go here for more information: http://bit.ly/2ska3pz
    private static final String REPLY = "Percebemos que publicou algo importante. " +
            "Por favor, acesse aqui para obter mais informações: http://bit.ly/2ska3pz";

    // Prevent the same tweet being replied to twice.
    private Map<Long, Boolean> ids;

    public TwitterBot() { }

    public void addToList(List<Long> list) {
        for (Long i : list) {
            if (!(ids.containsKey(i))) {
                ids.put(i, Boolean.FALSE);
            }
        }
    }

    public void addToList(Long id) {
        if (!(ids.containsKey(id))) {
            ids.put(id, Boolean.FALSE);
        }
    }

    private String getUserName(Long id) {
        // todo extend the tweet repo to find users by the tweet id.
        return "";
    }

    public void reply(Long id, String username) {
        // todo
        return;
    }


}
