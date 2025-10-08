// in src/features/match/hooks/useArenaData.ts

import { useState, useEffect } from 'react';
import { getArenaData } from '../services/matchService';
import type { ArenaData } from '../types/match';
import { AxiosError } from 'axios'; // Import AxiosError

export const useArenaData = (matchId: string | undefined) => {
    const [arenaData, setArenaData] = useState<ArenaData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    // --- ADD THIS NEW STATE ---
    const [shouldRedirect, setShouldRedirect] = useState(false);

    useEffect(() => {
        if (!matchId) {
            setError("No Match ID provided.");
            setIsLoading(false);
            return;
        }

        const fetchDetails = async () => {
            try {
                const data = await getArenaData(matchId);
                setArenaData(data);
            } catch (err: any) {
                // --- UPDATE THE CATCH BLOCK ---
                if (err instanceof AxiosError && err.response?.status === 409) {
                    // This is our specific "match completed" signal
                    console.log("Match is already completed. Signaling redirect.");
                    setShouldRedirect(true);
                } else {
                    // This is a different, unexpected error
                    setError(err.message || "Failed to load match data.");
                }
            } finally {
                setIsLoading(false);
            }
        };

        fetchDetails();
    }, [matchId]);

    // --- RETURN THE NEW STATE ---
    return { arenaData, isLoading, error, shouldRedirect };
};