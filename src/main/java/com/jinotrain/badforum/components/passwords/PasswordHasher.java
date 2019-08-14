package com.jinotrain.badforum.components.passwords;

import java.util.Optional;

public abstract class PasswordHasher
{
    protected class PrefixAndHash
    {
        public String prefix;
        public String hash;

        public PrefixAndHash(String p, String h)
        {
            prefix = p;
            hash   = h;
        }
    }


    protected abstract String getPrefix();
    protected abstract String hash(String password);
    protected abstract boolean checkHash(String password, String hash);


    protected Optional<PrefixAndHash> splitPrefixedHash(String prefixedHash)
    {
        int leftBracketPos  = prefixedHash.indexOf('{');
        int rightBracketPos = prefixedHash.indexOf('}', leftBracketPos);

        if (leftBracketPos == 0 && rightBracketPos != -1)
        {
            try
            {
                String prefix = prefixedHash.substring(leftBracketPos + 1, rightBracketPos);
                String hash   = prefixedHash.substring(rightBracketPos + 1);

                return Optional.of(new PrefixAndHash(prefix, hash));
            }
            catch (IndexOutOfBoundsException e)
            {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }


    public final String hashAndPrefix(String password)
    {
        return String.format("{%s}%s", getPrefix(), hash(password));
    }


    public final boolean hashMatches(String password, String passhash)
    {
        Optional<PrefixAndHash> prefixCheck = splitPrefixedHash(passhash);
        if (!prefixCheck.isPresent()) { return false; }

        PrefixAndHash prefixAndHash = prefixCheck.get();
        if (!prefixAndHash.prefix.equals(getPrefix())) { return false; }

        return checkHash(password, prefixAndHash.hash);
    }
}
