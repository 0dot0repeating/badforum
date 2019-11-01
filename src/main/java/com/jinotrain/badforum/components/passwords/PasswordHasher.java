package com.jinotrain.badforum.components.passwords;

public abstract class PasswordHasher
{
    protected class PrefixAndHash
    {
        String prefix;
        String hash;

        PrefixAndHash(String p, String h)
        {
            prefix = p;
            hash   = h;
        }
    }


    protected abstract String getPrefix();
    protected abstract String hash(String password);
    protected abstract String checkHash(String password, String hash);


    private PrefixAndHash splitPrefixedHash(String prefixedHash)
    {
        int leftBracketPos  = prefixedHash.indexOf('{');
        int rightBracketPos = prefixedHash.indexOf('}', leftBracketPos);

        if (leftBracketPos == 0 && rightBracketPos != -1)
        {
            try
            {
                String prefix = prefixedHash.substring(leftBracketPos + 1, rightBracketPos);
                String hash   = prefixedHash.substring(rightBracketPos + 1);

                return new PrefixAndHash(prefix, hash);
            }
            catch (IndexOutOfBoundsException e)
            {
                return null;
            }
        }

        return null;
    }


    final String hashAndPrefix(String password)
    {
        return "{" + getPrefix() +"}" + hash(password);
    }


    final String checkHashAndUpgrade(String password, String passhash)
    {
        PrefixAndHash prefixAndHash = splitPrefixedHash(passhash);
        if (prefixAndHash == null) { return null; }

        String prefix = prefixAndHash.prefix;
        String hash   = prefixAndHash.hash;

        if (!prefix.equals(getPrefix())) { return null; }

        String newHash = checkHash(password, hash);
        if (newHash != null) { return "{" + getPrefix() + "}" + newHash; }

        return null;
    }
}
