import { useState, useEffect } from 'react';
import { getProblemBySlug } from '../services/problemService';
import type { ProblemDetail } from '../types/problem';

export const useProblem = (slug: string) => {
    const [problem, setProblem] = useState<ProblemDetail | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchProblem = async () => {
            try {
                setIsLoading(true);
                setError(null);
                const data = await getProblemBySlug(slug);
                setProblem(data);
            } catch (err: any) {
                setError(err.message || 'Failed to fetch problem details.');
            } finally {
                setIsLoading(false);
            }
        };

        if (slug) {
            fetchProblem();
        }
    }, [slug]);

    return { problem, isLoading, error };
};