package com.jinotrain.badforum.components;

import com.jinotrain.badforum.util.PathFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Service
public class RegistrationTriviaService implements ApplicationListener<ContextRefreshedEvent>
{
    private class QuestionAndAnswers
    {
        String question;
        Set<String> answers;

        QuestionAndAnswers(String question, Set<String> answers)
        {
            this.question = question;
            this.answers  = answers;
        }
    }

    private static Logger logger = LoggerFactory.getLogger(RegistrationTriviaService.class);

    private List<QuestionAndAnswers> questionsAndAnswers = null;

    @Value("${badforum.triviafilename:registerQuestions.txt}")
    private String triviaFilename;


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        String jarPath = PathFinder.getJarDirectory();

        File triviaPath_jar = new File(jarPath, triviaFilename);
        File triviaPath_cwd = new File(triviaFilename);
        File triviaPath = triviaPath_jar.isFile() ? triviaPath_jar : triviaPath_cwd;

        try
        {
            questionsAndAnswers = buildQuestionsAndAnswers(triviaPath);
        }
        catch (FileNotFoundException e)
        {
            questionsAndAnswers = null;
            logger.info("Could not find registration question file (checked \"{}\", \"{}\")", triviaPath_jar.getAbsolutePath(), triviaPath_cwd.getAbsolutePath());
            logger.info("Defaulting to registration questions being disabled");
        }
    }


    private List<QuestionAndAnswers> buildQuestionsAndAnswers(File triviaPath) throws FileNotFoundException
    {
        List<QuestionAndAnswers> ret = new ArrayList<>();

        int questionCount = 0;
        int answerCount   = 0;

        try (FileInputStream inputStream = new FileInputStream(triviaPath))
        {
            //noinspection CharsetObjectCanBeUsed (since, y'know, java 8)
            Scanner scanner = new Scanner(inputStream, "UTF-8");

            String question = null;
            Set<String> answers = null;

            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine();

                if (line.startsWith("#")) { continue; }

                if (line.matches("^[\t \u000b]*$"))
                {
                    if (answers != null && !answers.isEmpty())
                    {
                        ret.add(new QuestionAndAnswers(question, answers));
                    }

                    question = null;
                    answers  = null;
                    continue;
                }

                if (question == null)
                {
                    question = line;
                    answers = new HashSet<>();
                    questionCount++;

                    logger.debug("Found question: \"{}\"", question);
                }
                else
                {
                    String answer = simplifyAnswerText(line);
                    answers.add(answer);
                    answerCount++;

                    logger.debug("  Found answer: \"{}\"", answer);
                }
            }

            // clear out what's left
            if (answers != null && !answers.isEmpty())
            {
                ret.add(new QuestionAndAnswers(question, answers));
            }

            logger.info("Found trivia file at \"{}\"", triviaPath.getAbsolutePath());
            logger.info("Loaded {} questions and {} answers", questionCount, answerCount);
        }
        catch (FileNotFoundException e)
        {
            throw e;
        }
        catch (IOException ignore) {} // failed to close

        return ret;
    }


    // removes all but whitespace/numbers/letters from a string, and makes letters lowercase
    //  also compresses whitespace into a single space, and strips trailing/leading whitespace
    private String simplifyAnswerText(String answer)
    {
        StringBuilder simplifiedBuilder = new StringBuilder(answer.length());

        boolean inWhitespace = false;
        boolean notEmpty     = false;

        for (int i = 0; i < answer.length(); i++)
        {
            int unichar = answer.codePointAt(i);

            // we only care about characters in the 0-127 (ASCII) range
            if (unichar > 127) { continue; }
            char asciiChar = (char)unichar;

            if (asciiChar == ' ' || asciiChar == '\t' || asciiChar == '\n' || asciiChar == '\r' || asciiChar == '\u000b')
            {
                inWhitespace = true;
                continue;
            }

            boolean isLetter = (asciiChar >= 'a' && asciiChar <= 'z') || (asciiChar >= 'A' && asciiChar <= 'Z');
            boolean isNumber = asciiChar >= '0' && asciiChar <= '9';
            if (!(isLetter || isNumber)) { continue; }

            // uppercase to lowercase
            if (isLetter && asciiChar <= 'Z') { asciiChar += ('a' - 'A'); }

            if (inWhitespace && notEmpty) { simplifiedBuilder.append(' '); }

            simplifiedBuilder.append(asciiChar);
            inWhitespace = false;
            notEmpty     = true;
        }

        return simplifiedBuilder.toString();
    }


    public int questionCount()
    {
        if (questionsAndAnswers == null) { return -1; }
        return questionsAndAnswers.size();
    }


    public String getQuestion(int index)
    {
        if (index < 0 || questionsAndAnswers == null || index >= questionsAndAnswers.size())
        {
            return null;
        }

        return questionsAndAnswers.get(index).question;
    }


    public boolean validAnswerForQuestion(int index, String answer)
    {
        if (index < 0 || questionsAndAnswers == null || index >= questionsAndAnswers.size())
        {
            return false;
        }

        String strippedAnswer = simplifyAnswerText(answer);
        Set<String> answers = questionsAndAnswers.get(index).answers;
        return answers.contains(strippedAnswer);
    }
}
