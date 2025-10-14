import { useState, useEffect } from 'react';
import { getArenaData } from '../services/matchService';
import type { ArenaData } from '../types/match';
import { AxiosError } from 'axios';

export const useArenaData = (matchId: string | undefined) => {
    const [arenaData, setArenaData] = useState<ArenaData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

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

                if (err instanceof AxiosError && err.response?.status === 409) {

                    console.log("Match is already completed. Signaling redirect.");
                    setShouldRedirect(true);
                } else {
                    setError(err.message || "Failed to load match data.");
                }
            } finally {
                setIsLoading(false);
            }
        };

        fetchDetails();
    }, [matchId]);


    return { arenaData, isLoading, error, shouldRedirect };
};