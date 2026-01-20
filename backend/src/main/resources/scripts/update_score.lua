local key = KEYS[1]
local userHandle = ARGV[1]
local problemId = ARGV[2]
local verdict = ARGV[3]
local currentTimestamp = tonumber(ARGV[4]) -- This is "NOW" in epoch seconds
local execTime = tonumber(ARGV[5]) or 0
local memory = tonumber(ARGV[6]) or 0

local PENALTY_PER_WRONG_ATTEMPT = 600

local rawData = redis.call('GET', key)
if not rawData then return nil end
local data = cjson.decode(rawData)

-- 1. Calculate Relative Time
local startTime = tonumber(data['startTime']) or currentTimestamp
local relativeTime = currentTimestamp - startTime
if relativeTime < 0 then relativeTime = 0 end

-- 2. Ensure Scoreboard Structure
if not data['scoreboard'] then data['scoreboard'] = {} end
if not data['scoreboard']['users'] then
    data['scoreboard']['users'] = { ['@class'] = 'java.util.HashMap' }
end

-- 3. Initialize User Stats
if not data['scoreboard']['users'][userHandle] then
    data['scoreboard']['users'][userHandle] = {
        ['@class'] = 'com.Abhinav.backend.features.duel.model.DuelScoreboard$DuelUserStats',
        solved = 0,
        penalty = 0,
        problems = { ['@class'] = 'java.util.HashMap' }
    }
end

local userStats = data['scoreboard']['users'][userHandle]
local problems = userStats['problems']

-- 4. Initialize Problem Stats
if not problems[problemId] then
    problems[problemId] = {
        ['@class'] = 'com.Abhinav.backend.features.duel.model.DuelScoreboard$ProblemStats',
        status = "NONE",
        attempts = 0,
        bestTime = 0,
        -- Initialize history as a LinkedHashMap (Tagged)
        history = { ['@class'] = 'java.util.LinkedHashMap' }
    }
end

local problemStats = problems[problemId]

-- 5. Create Submission Object
local newSubmission = {
    ['@class'] = 'com.Abhinav.backend.features.duel.model.DuelScoreboard$SubmissionData',
    verdict = verdict,
    timeConsumedMillis = execTime,
    memoryConsumedBytes = memory,
    submissionTimeSeconds = relativeTime
}

-- 6. Add to History Map
-- Use the current attempt count + 1 as the key (String format)
-- We need to calculate size. Since we track 'attempts', we can use that for keys.
-- However, 'attempts' only counts WRONG answers usually.
-- Let's just use a manual counter or simple table size approach for the key.
local historyKey = tostring(problemStats['attempts'] + 1)
-- If we already have entries (e.g. from compilation errors not counted in attempts),
-- we need a unique key.
-- Safer: Count keys in history (simple iteration)
local count = 0
for _ in pairs(problemStats['history']) do count = count + 1 end
-- Subtract 1 because @class is a key
if problemStats['history']['@class'] then count = count - 1 end
historyKey = tostring(count + 1)

problemStats['history'][historyKey] = newSubmission

-- 7. Update Stats
if problemStats['status'] ~= "OK" then
    if verdict == "OK" then
        problemStats['status'] = "OK"
        problemStats['attempts'] = problemStats['attempts'] + 1
        problemStats['bestTime'] = relativeTime

        userStats['solved'] = userStats['solved'] + 1
        local wrongAttempts = problemStats['attempts'] - 1
        local extraPenalty = wrongAttempts * PENALTY_PER_WRONG_ATTEMPT
        userStats['penalty'] = userStats['penalty'] + relativeTime + extraPenalty
    else
        if verdict ~= "COMPILATION_ERROR" then
             problemStats['attempts'] = problemStats['attempts'] + 1
        end
    end
end

-- Save
local encoded = cjson.encode(data)
redis.call('SET', key, encoded, 'KEEPTTL')

return encoded